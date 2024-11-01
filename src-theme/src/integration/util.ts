import type { Module, GroupedModules } from "./types"

export function groupByCategory(modules: Module[]): GroupedModules {
    return modules.reduce((acc: GroupedModules, current: Module) => {
        const { category } = current;
        if (!acc[category]) {
            acc[category] = [];
        }
        acc[category].push(current);
        return acc;
    }, {});
}

export function rgbaToInt(rgba: number[]): number {
    const [r, g, b, a] = rgba;
    return (
        ((a & 0xff) << 24) |
        ((r & 0xff) << 16) |
        ((g & 0xff) << 8) |
        ((b & 0xff) << 0)
    );
}

export function rgbaToHex(rgba: number[]): string {
    const [r, g, b, a] = rgba;
    const alpha = a === 255 ? "" : a.toString(16).padStart(2, "0");
    return `#${r.toString(16).padStart(2, "0")}${g
        .toString(16)
        .padStart(2, "0")}${b.toString(16).padStart(2, "0")}${alpha}`;
}

export function intToRgba(value: number): number[] {
    const red = (value >> 16) & 0xff;
    const green = (value >> 8) & 0xff;
    const blue = (value >> 0) & 0xff;
    const alpha = (value >> 24) & 0xff;
    return [red, green, blue, alpha];
}

export function debounce<T extends (...args: any[]) => any>(func: T, delay: number = 300): (...args: Parameters<T>) => void {
    let timer: ReturnType<typeof setTimeout>;

    return (...args: Parameters<T>): void => {
        clearTimeout(timer);

        timer = setTimeout(() => {
            func(...args);
        }, delay);
    };
}

export function debounceAsync<T extends (...args: any[]) => Promise<any>>(func: T, delay: number = 300): (...args: Parameters<T>) => Promise<ReturnType<T>> {
    let timer: ReturnType<typeof setTimeout>;
    let resolveFunc: (value: ReturnType<T>) => void;
    let rejectFunc: (reason?: any) => void;

    return (...args: Parameters<T>): Promise<ReturnType<T>> => {
        clearTimeout(timer);

        return new Promise((resolve, reject) => {
            resolveFunc = resolve;
            rejectFunc = reject;

            timer = setTimeout(async () => {
                try {
                    const result = await func(...args);
                    resolveFunc(result);
                } catch (error) {
                    rejectFunc(error);
                }
            }, delay);
        });
    };
}

