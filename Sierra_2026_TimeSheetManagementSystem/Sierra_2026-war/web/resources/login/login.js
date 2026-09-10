(function () {
    "use strict";

    function initializeConsentRequirement() {
        const consentCheckbox = document.getElementById(
                "data_protection_acknowledgement");
        const signInButton = document.getElementById("sign_in_button");

        if (!consentCheckbox || !signInButton) {
            return;
        }

        function updateButtonState() {
            signInButton.disabled = !consentCheckbox.checked;
        }

        consentCheckbox.addEventListener("change", updateButtonState);
        updateButtonState();
    }

    if (document.readyState === "loading") {
        document.addEventListener(
                "DOMContentLoaded",
                initializeConsentRequirement);
    } else {
        initializeConsentRequirement();
    }
}());
