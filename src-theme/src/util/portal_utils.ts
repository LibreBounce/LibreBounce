export function portal(node: HTMLElement, target: HTMLElement = document.body) {
    target.appendChild(node);
    return {
        destroy() {
            if (node.parentNode) node.parentNode.removeChild(node);
        }
    };
}
