(function () {
    "use strict";

    const STORAGE_KEY = "sierra-theme";
    const DARK = "dark";
    const LIGHT = "light";
    const systemPreference = window.matchMedia("(prefers-color-scheme: dark)");

    function savedTheme() {
        try {
            const theme = localStorage.getItem(STORAGE_KEY);
            return theme === DARK || theme === LIGHT ? theme : null;
        } catch (error) {
            return null;
        }
    }

    function preferredTheme() {
        const saved = savedTheme();
        if (saved) {
            return saved;
        }

        return systemPreference.matches ? DARK : LIGHT;
    }

    function applyTheme(theme) {
        document.documentElement.dataset.theme = theme;

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const darkMode = theme === DARK;
            const label = darkMode
                    ? button.dataset.lightLabel : button.dataset.darkLabel;

            button.setAttribute("aria-label", label);
            button.setAttribute("title", label);
            button.setAttribute("aria-pressed", String(darkMode));
            button.querySelector(".pi").className = darkMode
                    ? "pi pi-sun" : "pi pi-moon";
        });
    }

    applyTheme(preferredTheme());

    document.addEventListener("DOMContentLoaded", function () {
        applyTheme(preferredTheme());
    });

    if (!window.sierraThemeToggleBound) {
        document.addEventListener("click", function (event) {
            const button = event.target.closest("[data-theme-toggle]");
            if (!button) {
                return;
            }

            const nextTheme = document.documentElement.dataset.theme === DARK
                    ? LIGHT : DARK;

            try {
                localStorage.setItem(STORAGE_KEY, nextTheme);
            } catch (error) {
                // The current page can still switch themes when storage is blocked.
            }
            applyTheme(nextTheme);
        });
        window.sierraThemeToggleBound = true;
    }

    window.addEventListener("storage", function (event) {
        if (event.key === STORAGE_KEY) {
            applyTheme(preferredTheme());
        }
    });

    systemPreference.addEventListener("change", function () {
        if (!savedTheme()) {
            applyTheme(preferredTheme());
        }
    });
}());
