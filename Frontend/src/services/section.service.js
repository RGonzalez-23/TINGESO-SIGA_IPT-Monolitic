import apiClient from './http-common';

/**
 * Service to handle section and academic offer requests.
 * Layer: Frontend Service.
 */
class SectionService {
  /**
   * Retrieves sections with optional query params (periodId, studyPlanId, teacherRun).
   * @param {Object} params
   */
  getAll(params = {}) {
    return apiClient.get('/sections', { params });
  }

  /**
   * Retrieves a single section by ID.
   * @param {number|string} id
   */
  getById(id) {
    return apiClient.get(`/sections/${id}`);
  }

  /**
   * Creates a new section.
   * @param {Object} data { courseId, academicPeriodId, teacherRun, capacity, timeBlockCodes }
   */
  create(data) {
    return apiClient.post('/sections', data);
  }

  /**
   * Updates an existing section.
   * @param {number|string} id
   * @param {Object} data { teacherRun, capacity, timeBlockCodes }
   */
  update(id, data) {
    return apiClient.put(`/sections/${id}`, data);
  }

  /**
   * Deletes a section. Prohibited if students are enrolled.
   * @param {number|string} id
   */
  delete(id) {
    return apiClient.delete(`/sections/${id}`);
  }
}

export default new SectionService();
