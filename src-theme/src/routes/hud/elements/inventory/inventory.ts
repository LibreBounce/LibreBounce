import {readable} from "svelte/store";

export const effectFrame = readable('', (set) => {
    // TODO
    const getUrl = (i: number) => `https://raw.githubusercontent.com/Altpapier/GlintCreator/refs/heads/master/glint/FULL_GLINT/Frame_${i.toString().padStart(4, '0')}.png`;

    let value = 0;

    const interval = setInterval(() => {
        value = (value + 1) % 825;
        set(getUrl(value + 1));
    }, 50);

    return () => clearInterval(interval);
});
