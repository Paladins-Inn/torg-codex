(function () {
    "use strict";

    function initThumbnail(img) {
        var frame = img.closest(".publication-thumbnail");
        if (!frame) {
            return;
        }

        function markLoaded() {
            frame.classList.add("is-loaded");
        }

        function handleError() {
            var fallback = img.getAttribute("data-fallback-src");
            img.removeAttribute("data-fallback-src");
            if (fallback) {
                img.src = fallback;
            } else {
                frame.classList.remove("is-loaded");
            }
        }

        img.addEventListener("load", function () {
            if (img.naturalWidth > 0) {
                markLoaded();
            } else {
                handleError();
            }
        });

        img.addEventListener("error", handleError);

        if (img.complete) {
            if (img.naturalWidth > 0) {
                markLoaded();
            } else {
                handleError();
            }
        }
    }

    function initAll() {
        var images = document.querySelectorAll(".publication-thumbnail .publication-thumbnail-img");
        for (var i = 0; i < images.length; i++) {
            initThumbnail(images[i]);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initAll);
    } else {
        initAll();
    }
})();
