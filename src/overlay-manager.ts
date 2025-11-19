/**
 * OverlayManager
 *
 * Singleton utility for managing a fullscreen black overlay during native operations.
 * Prevents visual flicker when transitioning between image input and cropping views.
 */
class OverlayManagerClass {
  private overlay: HTMLDivElement | null = null;
  private fadeTimeout: number | null = null;

  /**
   * Shows the fullscreen black overlay with fade-in animation
   */
  show(): void {
    // Prevent duplicate overlays
    if (this.overlay) {
      return;
    }

    // Clear any pending hide animations
    if (this.fadeTimeout) {
      window.clearTimeout(this.fadeTimeout);
      this.fadeTimeout = null;
    }

    this.overlay = document.createElement('div');

    // Apply inline styles with safe area support
    // Using env() CSS variables ensures safe areas are respected on all platforms
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

    // Trigger fade-in animation
    requestAnimationFrame(() => {
      if (this.overlay) {
        this.overlay.style.opacity = '1';
      }
    });
  }

  /**
   * Hides the overlay with fade-out animation and removes from DOM
   */
  hide(): void {
    if (!this.overlay) {
      return;
    }

    // Start fade-out
    this.overlay.style.transition = 'opacity 200ms ease-out';
    this.overlay.style.opacity = '0';

    // Remove from DOM after animation completes
    this.fadeTimeout = window.setTimeout(() => {
      if (this.overlay && this.overlay.parentNode) {
        this.overlay.parentNode.removeChild(this.overlay);
      }
      this.overlay = null;
      this.fadeTimeout = null;
    }, 200);
  }

  /**
   * Checks if overlay is currently visible
   */
  isVisible(): boolean {
    return this.overlay !== null;
  }

  /**
   * Force removes the overlay immediately without animation
   * Use only in emergency cleanup scenarios
   */
  forceHide(): void {
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
