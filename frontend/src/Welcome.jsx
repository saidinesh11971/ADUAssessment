import { useEffect, useState } from "react";
import aduLogo from "./assets/ADU_logo.jpg";

const AUTO_DISMISS_MS = 4500;
const EXIT_ANIMATION_MS = 700;

export default function Welcome({ onDone }) {
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    const autoTimer = setTimeout(triggerExit, AUTO_DISMISS_MS);
    return () => clearTimeout(autoTimer);
  }, []);

  function triggerExit() {
    if (exiting) return;
    setExiting(true);
    setTimeout(onDone, EXIT_ANIMATION_MS);
  }

  return (
    <div className={`welcome ${exiting ? "welcome--exiting" : ""}`}>
      <div className="welcome__glow" aria-hidden="true" />
      <div className="welcome__inner">
        <div className="welcome__logo">
          <img src={aduLogo} alt="Appliance Distributors Unlimited" />
        </div>
        <p className="welcome__eyebrow">Welcome to</p>
        <h1 className="welcome__title">Appliance Distributors Unlimited</h1>
        <p className="welcome__tagline">
          Trusted Since 1982. Preferred for Generations.
        </p>
        <p className="welcome__copy">
          Delivering premium appliances, expert solutions, and strong
          partnerships for customers, dealers, builders, and vendors. Together,
          we create spaces that inspire.
        </p>
        <button className="welcome__cta" onClick={triggerExit}>
          Enter Importer
        </button>
      </div>
    </div>
  );
}
