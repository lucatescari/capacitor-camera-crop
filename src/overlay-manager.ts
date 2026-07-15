/**
 * OverlayManager
 *
 * Singleton utility for managing a fullscreen black overlay during native operations.
 * Prevents visual flicker when transitioning between image input and cropping views.
 *
 * Reference-counted: overlapping captureAndCrop() calls share one overlay, and it is
 * only torn down once the last in-flight call finishes, so a fast call finishing first
 * can't remove the overlay while another call is still running.
 */
class OverlayManagerClass {
  private overlay: HTMLDivElement | null = null;
  private fadeTimeout: number | null = null;
  private activeCount = 0;

  /**
   * Shows the fullscreen black overlay with fade-in animation.
   */
  show(): void {
    this.activeCount++;

    // Overlay already present (another call is in flight) — nothing more to do.
    if (this.overlay) {
      return;
    }

    // Clear any pending hide animation.
    if (this.fadeTimeout) {
      window.clearTimeout(this.fadeTimeout);
      this.fadeTimeout = null;
    }

    this.overlay = document.createElement('div');

    // Apply inline styles with safe area support.
    // Using env() CSS variables ensures safe areas are respected on all platforms.
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

    // Trigger fade-in animation.
    requestAnimationFrame(() => {
      if (this.overlay) {
        this.overlay.style.opacity = '1';
      }
    });
  }

  /**
   * Hides the overlay once the last in-flight operation finishes, with a fade-out
   * animation, and removes it from the DOM.
   */
  hide(): void {
    if (this.activeCount > 0) {
      this.activeCount--;
    }

    // Other calls are still active, or there's nothing to hide.
    if (this.activeCount > 0 || !this.overlay) {
      return;
    }

    // Start fade-out.
    this.overlay.style.transition = 'opacity 200ms ease-out';
    this.overlay.style.opacity = '0';

    // Remove from DOM after animation completes.
    this.fadeTimeout = window.setTimeout(() => {
      if (this.overlay && this.overlay.parentNode) {
        this.overlay.parentNode.removeChild(this.overlay);
      }
      this.overlay = null;
      this.fadeTimeout = null;
    }, 200);
  }
}

export const OverlayManager = new OverlayManagerClass();
