import apiClient from './http-common';

/**
 * Service to handle career-related API requests.
 */
class CareerDataService {
  /**
   * Retrieves all careers (both active and inactive).
   */
  getAllCareers() {
    return apiClient.get('/careers');
  }

  /**
   * Retrieves all active careers.
   */
  getActiveCareers() {
    return apiClient.get('/careers?activeOnly=true');
  }

  /**
   * Retrieves a career by its code.
   * @param {string} code
   */
  getCareerByCode(code) {
    return apiClient.get(`/careers/${code}`);
  }

  /**
   * Registers a new career with initial active study plan.
   * @param {Object} data
   */
  registerCareer(data) {
    return apiClient.post('/careers', data);
  }

  /**
   * Updates an existing career.
   * @param {string} code
   * @param {Object} data
   */
  updateCareer(code, data) {
    return apiClient.put(`/careers/${code}`, data);
  }

  /**
   * Deletes a career if it has no plans or students.
   * @param {string} code
   */
  deleteCareer(code) {
    return apiClient.delete(`/careers/${code}`);
  }
}

export default new CareerDataService();
