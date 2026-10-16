import http from './http-common';

/**
 * periodCloseService handles pre-audit and semester closure operations.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
const periodCloseService = {
  checkCloseReadiness: (periodId) => http.get(`/academic-periods/${periodId}/close-readiness`),
  executeClose: (periodId) => http.post(`/academic-periods/${periodId}/close`),
};

export default periodCloseService;
