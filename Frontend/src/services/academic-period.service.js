import apiClient from './http-common';

/**
 * Service to handle academic period requests (e.g. "2027-1").
 * Layer: Frontend Service.
 */
class AcademicPeriodService {
  /**
   * Retrieves all academic periods.
   */
  getAll() {
    return apiClient.get('/academic-periods');
  }

  /**
   * Retrieves an academic period by ID.
   * @param {string} id format "YYYY-S" (e.g., "2027-1")
   */
  getById(id) {
    return apiClient.get(`/academic-periods/${id}`);
  }

  /**
   * Creates a new academic period in initial ABIERTO state.
   * @param {Object} data { year: number, semester: number }
   */
  create(data) {
    return apiClient.post('/academic-periods', data);
  }

  /**
   * Toggles open/closed state of an academic period.
   * @param {string} id
   */
  toggleStatus(id) {
    return apiClient.patch(`/academic-periods/${id}/toggle-status`);
  }
}

export default new AcademicPeriodService();
