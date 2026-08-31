import { toast } from "../components/ui.js?v=1.0.0";

/** Lets every existing image file input accept clipboard images without changing its upload flow. */
export function enableImagePaste() {
  document.addEventListener("paste", event => {
    const pasted = [...(event.clipboardData?.items || [])]
      .filter(item => item.kind === "file" && item.type.startsWith("image/"))
      .map(item => item.getAsFile())
      .filter(Boolean);
    if (!pasted.length) return;

    const scope = [...document.querySelectorAll(".board-modal-overlay")].at(-1)
      || document.querySelector("dialog[open]") || document;
    const inputs = [...scope.querySelectorAll('input[type="file"]')]
      .filter(input => acceptsImages(input));
    const input = inputs.at(-1);
    if (!input) return;

    const transfer = new DataTransfer();
    const existing = input.multiple ? [...input.files] : [];
    [...existing, ...pasted].slice(0, input.multiple ? undefined : 1)
      .forEach(file => transfer.items.add(file));
    input.files = transfer.files;
    input.dispatchEvent(new Event("change", {bubbles: true}));
    event.preventDefault();
    toast(`已从剪贴板放入 ${input.files.length} 张图片`);
  });
}

function acceptsImages(input) {
  const accept = (input.accept || "").toLowerCase();
  return accept.includes("image/") || /jpeg|jpg|png|webp|gif/.test(accept);
}
