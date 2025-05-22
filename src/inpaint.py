
import os
import numpy as np
from PIL import Image
import torch
import onnxruntime

# Setup ONNX model
onnx_model_path = 'src/lama_fp32.onnx'
if not os.path.exists(onnx_model_path):
    os.system(f"wget https://huggingface.co/Carve/LaMa-ONNX/resolve/main/{onnx_model_path}")

sess_options = onnxruntime.SessionOptions()
rmodel = onnxruntime.InferenceSession(onnx_model_path, sess_options=sess_options)

# Helper Functions
def get_image(image):
    if isinstance(image, Image.Image):
        img = np.array(image)
    elif isinstance(image, np.ndarray):
        img = image.copy()
    else:
        raise Exception("Input should be PIL.Image or np.ndarray")
    if img.ndim == 3:
        img = np.transpose(img, (2, 0, 1))  # CHW
    elif img.ndim == 2:
        img = img[np.newaxis, ...]
    img = img.astype(np.float32) / 255
    return img

def ceil_modulo(x, mod):
    return x if x % mod == 0 else (x // mod + 1) * mod

def pad_img_to_modulo(img, mod):
    c, h, w = img.shape
    oh, ow = ceil_modulo(h, mod), ceil_modulo(w, mod)
    return np.pad(img, ((0, 0), (0, oh - h), (0, ow - w)), mode="symmetric")

def prepare_img_and_mask(image, mask, device='cpu', pad_mod=8):
    img = get_image(image)
    msk = get_image(mask)
    img = pad_img_to_modulo(img, pad_mod)
    msk = pad_img_to_modulo(msk, pad_mod)
    img_tensor = torch.from_numpy(img).unsqueeze(0).to(device)
    msk_tensor = torch.from_numpy(msk).unsqueeze(0).to(device)
    msk_tensor = (msk_tensor > 0).float()
    return img_tensor, msk_tensor

def inpaint(image_path, mask_path, output_path):
    orig_image = Image.open(image_path).convert("RGB")
    orig_mask = Image.open(mask_path).convert("L")

    # Resize to LaMa-friendly size (optional: 512x512)
    resized_image = orig_image.resize((512, 512))
    resized_mask = orig_mask.resize((512, 512))

    input_tensor, mask_tensor = prepare_img_and_mask(resized_image, resized_mask)

    outputs = rmodel.run(
        None,
        {
            'image': input_tensor.numpy().astype(np.float32),
            'mask': mask_tensor.numpy().astype(np.float32),
        }
    )

    result = outputs[0][0]
    result = result.transpose(1, 2, 0).astype(np.uint8)

    result_img = Image.fromarray(result).resize(orig_image.size)
    result_img.save(output_path)
    print(f"[Done] Inpainted image saved to: {output_path}")


# CLI Interface
if __name__ == "__main__":
    inpaint("input_image.png", "input_mask.png", "output_image.png")