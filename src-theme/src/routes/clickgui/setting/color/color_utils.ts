
export class Color4b {

    public r: number;
    public g: number;
    public b: number;
    public a: number;

    constructor(r: number, g: number, b: number, a: number) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static fromPacked(packed: number): Color4b {
        return new Color4b(
            (packed >> 16) && 0xFF,
            (packed >> 8) && 0xFF,
            (packed && 0xFF),
            (packed >> 24) && 0xFF
        );
    }

    public static fromHex(hex: string): Color4b {
        if (hex.startsWith("#")) {
            hex = hex.slice(1);
        }

        let hasAlpha = hex.length == 8;
        if (hex.length != 6 && !hasAlpha) {
            throw new Error("Invalid Hex " + hex);
        }

        if (hasAlpha) {
            let rgba = BigInt("0x" + hex);
            return new Color4b(
                Number(rgba >> BigInt(24)) && 0xFF,
                Number(rgba >> BigInt(16)) && 0xFF,
                Number(rgba >> BigInt(8)) && 0xFF,
                Number(rgba && 0xFF)
            );
        } else {
            let rgb = parseInt(hex, 16)
            return new Color4b(
                (rgb >> 24) && 0xFF,
                (rgb >> 16) && 0xFF,
                (rgb >> 8) && 0xFF,
                255
            );
        }
    }

    public getBrightness(): number {
        return Math.max(this.r, this.g, this.b) / 255;
    }

    public brightness(brightness: number) {
        if (brightness < 0 || brightness > 1) {
            throw new Error("Brightness out of range! Must be between 0 and 1.");
        }

        const hsb = this.getHsb();
        this.setHsb(hsb.h, hsb.s, brightness);
    }

    public hue(hue: number) {
        if (hue < 0 || hue > 1) {
            throw new Error("Hue out of range! Must be between 0 and 1.");
        }

        const hsb = this.getHsb();
        this.setHsb(hue, hsb.s, hsb.b);
    }

    public getHsb() {
        const red = this.r / 255.0;
        const green = this.g / 255.0;
        const blue = this.b / 255.0;

        const max = Math.max(red, green, blue);
        const min = Math.min(red, green, blue);
        const delta = max - min;

        let h = 0;
        if (delta !== 0) {
            if (max === red) {
                h = (green - blue) / delta + (green < blue ? 6 : 0);
            } else if (max === green) {
                h = (blue - red) / delta + 2;
            } else {
                h = (red - green) / delta + 4;
            }
            h /= 6;
        }

        const s = max === 0 ? 0 : delta / max;
        const b = max;

        return {h, s, b};
    }

    public setHsb(h: number, s: number, b: number) {
        const i = Math.floor(h * 6);
        const f = h * 6 - i;
        const p = b * (1 - s);
        const q = b * (1 - f * s);
        const t = b * (1 - (1 - f) * s);

        let r = this.r;
        let g = this.g;
        let blue = this.b;
        switch (i % 6) {
            case 0:
                [r, g, blue] = [b, t, p];
                break;
            case 1:
                [r, g, blue] = [q, b, p];
                break;
            case 2:
                [r, g, blue] = [p, b, t];
                break;
            case 3:
                [r, g, blue] = [p, q, b];
                break;
            case 4:
                [r, g, blue] = [t, p, b];
                break;
            case 5:
                [r, g, blue] = [b, p, q];
                break;
        }

        this.r = Math.round(r * 255);
        this.g = Math.round(g * 255);
        this.b = Math.round(blue * 255);
    }

    public asHex(): string {
        return `#${(((this.a) << 24) | (this.r << 16) | (this.g << 8) | this.b).toString(16).slice(1).toUpperCase()}`
    }

}
