/**
 * Utility functions for Chilean National ID (RUN / RUT) validation and formatting.
 * Uses Modulo 11 check digit algorithm.
 */
export const runValidator = {
  /**
   * Calculates the expected check digit (DV) for a given RUN body.
   * @param {string} body RUN body without dots or hyphen
   * @returns {string} calculated DV ('0'-'9' or 'K')
   */
  calculateDV: (body) => {
    if (!body || !/^[0-9]+$/.test(body)) return '';
    let sum = 0;
    let mul = 2;
    for (let i = body.length - 1; i >= 0; i--) {
      sum += parseInt(body[i], 10) * mul;
      mul = mul === 7 ? 2 : mul + 1;
    }
    const rem = 11 - (sum % 11);
    if (rem === 11) return '0';
    if (rem === 10) return 'K';
    return rem.toString();
  },

  /**
   * Validates a Chilean RUN with Modulo 11.
   * @param {string} run formatted or raw RUN
   * @returns {boolean} true if valid
   */
  validate: (run) => {
    if (!run) return false;
    const cleaned = run.trim().toUpperCase().replace(/\./g, '');
    if (!/^[0-9]{7,8}-[0-9K]$/.test(cleaned)) return false;
    const [body, dv] = cleaned.split('-');
    return runValidator.calculateDV(body) === dv;
  },

  /**
   * Cleans dots and standardizes to uppercase with hyphen (e.g. 11111111-1).
   * @param {string} run
   * @returns {string} cleaned RUN
   */
  clean: (run) => {
    if (!run) return '';
    return run.trim().toUpperCase().replace(/\./g, '');
  },

  /**
   * Automatically formats input as body-DV (e.g., typing '111111111' -> '11111111-1').
   * @param {string} run
   * @returns {string} formatted RUN
   */
  format: (run) => {
    if (!run) return '';
    let value = run.replace(/[^0-9kK]/g, '').toUpperCase();
    if (value.length > 9) value = value.slice(0, 9);
    if (value.length > 1) {
      const dv = value.slice(-1);
      const body = value.slice(0, -1);
      return `${body}-${dv}`;
    }
    return value;
  },
};

/**
 * Sanitizes a string for institutional email:
 * Removes accents, tildes (diacritical marks), converts to lowercase,
 * and strips any non-alphanumeric characters.
 * Example: 'González' -> 'gonzalez', 'Álvarez' -> 'alvarez', 'Peña' -> 'pena'.
 * @param {string} text
 * @returns {string} sanitized string without accents or special characters
 */
export const sanitizeForEmail = (text) => {
  if (!text) return '';
  return text
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '');
};
