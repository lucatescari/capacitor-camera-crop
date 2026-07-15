class OverlayManagerClass {
    overlay = null;
    fadeTimeout = null;
    show() {
        if (this.overlay) {
            return;
        }
        if (this.fadeTimeout) {
            window.clearTimeout(this.fadeTimeout);
            this.fadeTimeout = null;
        }
        this.overlay = document.createElement('div');
        Object.assign(this.overlay.style, {
            position: 'fixed',
            top: 'env(safe-area-inset-top, 0px)',
            right: 'env(safe-area-inset-right, 0px)',
            bottom: 'env(safe-area-inset-bottom, 0px)',
            left: 'env(safe-area-inset-left, 0px)',
            backgroundColor: 'black',
            zIndex: '999999',
            opacity: '0',
            transition: 'opacity 150ms ease-in',
        });
        document.body.appendChild(this.overlay);
        requestAnimationFrame(() => {
            if (this.overlay) {
                this.overlay.style.opacity = '1';
            }
        });
    }
    hide() {
        if (!this.overlay) {
            return;
        }
        this.overlay.style.transition = 'opacity 200ms ease-out';
        this.overlay.style.opacity = '0';
        this.fadeTimeout = window.setTimeout(() => {
            if (this.overlay && this.overlay.parentNode) {
                this.overlay.parentNode.removeChild(this.overlay);
            }
            this.overlay = null;
            this.fadeTimeout = null;
        }, 200);
    }
    isVisible() {
        return this.overlay !== null;
    }
    forceHide() {
        if (this.fadeTimeout) {
            window.clearTimeout(this.fadeTimeout);
            this.fadeTimeout = null;
        }
        if (this.overlay && this.overlay.parentNode) {
            this.overlay.parentNode.removeChild(this.overlay);
        }
        this.overlay = null;
    }
}
export const OverlayManager = new OverlayManagerClass();
//# sourceMappingURL=overlay-manager.js.map