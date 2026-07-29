import { describe, expect, test } from "vitest";

describe("test environment", () => {
    test("Vitest runs successfully", () => {
        expect(1+1).toBe(2);
    });

    test("jsdom is available", () => {
        const element = document.createElement("div");
        element.textContent = "Library System";

        expect(element).toHaveTextContent("Library System");
    });
});