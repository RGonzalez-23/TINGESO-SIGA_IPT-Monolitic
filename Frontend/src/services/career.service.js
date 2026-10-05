import apiClient from './http-common';

/**
 * Service to handle career-related API requests.
 */
class CareerDataService {
  /**
   * Retrieves all active careers.
   */
  getActiveCareers() {
    return apiClient.get('/careers');
  }
}

export default new CareerDataService();
