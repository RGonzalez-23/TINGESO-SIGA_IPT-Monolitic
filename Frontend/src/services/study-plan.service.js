import apiClient from './http-common';

/**
 * Service to handle study plan API requests.
 */
class StudyPlanDataService {
  /**
   * Retrieves all study plans for a specific career code.
   * @param {string} careerCode
   */
  getPlansByCareer(careerCode) {
    return apiClient.get(`/study-plans?careerCode=${careerCode}`);
  }

  /**
   * Retrieves a study plan by ID.
   * @param {number|string} id
   */
  getStudyPlanById(id) {
    return apiClient.get(`/study-plans/${id}`);
  }

  /**
   * Creates a new study plan for a career.
   * @param {Object} data - { code, careerCode, isActive }
   */
  createStudyPlan(data) {
    return apiClient.post('/study-plans', data);
  }

  /**
   * Activates a study plan as the active curriculum for its career.
   * @param {number|string} id
   */
  activateStudyPlan(id) {
    return apiClient.put(`/study-plans/${id}/activate`);
  }

  /**
   * Deletes a study plan if it has no enrolled students.
   * @param {number|string} id
   */
  deleteStudyPlan(id) {
    return apiClient.delete(`/study-plans/${id}`);
  }
}

export default new StudyPlanDataService();
