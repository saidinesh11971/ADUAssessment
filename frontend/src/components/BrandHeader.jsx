import aduLogo from "../assets/ADU_logo.jpg";
import { MAX_FILE_SIZE_MB } from "../constants.js";

export default function BrandHeader() {
  return (
    <header className="brand-header">
      <div className="brand-mark">
        <img src={aduLogo} alt="ADU" />
      </div>
      <div>
        <h1 className="brand-title">AVB Closeout Importer</h1>
        <p className="subtitle">
          Upload an Excel workbook (.xlsx, up to {MAX_FILE_SIZE_MB} MB) to
          import rows into <code>avb_closeout</code>.
        </p>
      </div>
    </header>
  );
}
