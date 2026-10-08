import http from './http-common';

/**
 * teacher.service.js provides API communication with backend teacher endpoints.
 * Layer: Frontend Service.
 */
const teacherService = {
  /**
   * Retrieves all teachers with optional filters.
   * @param {Object} params { active?: boolean, search?: string }
   */
  getAll: (params = {}) => {
    return http.get('/teachers', { params });
  },

  /**
   * Retrieves a single teacher by RUN.
   * @param {string} run teacher RUN with hyphen
   */
  getByRun: (run) => {
    return http.get(`/teachers/${run}`);
  },

  /**
   * Registers a new teacher.
   * @param {Object} data TeacherRegistrationDTO
   */
  create: (data) => {
    return http.post('/teachers', data);
  },

  /**
   * Updates an existing teacher.
   * @param {string} run teacher RUN
   * @param {Object} data TeacherUpdateDTO
   */
  update: (run, data) => {
    return http.put(`/teachers/${run}`, data);
  },

  /**
   * Deletes a teacher physically (allowed only if no course sections exist).
   * @param {string} run teacher RUN
   */
  delete: (run) => {
    return http.delete(`/teachers/${run}`);
  },

  /**
   * Previews the unique institutional email calculated with collision resolution.
   * @param {string} firstName
   * @param {string} paternalLastName
   * @param {string} maternalLastName
   */
  previewEmail: (firstName, paternalLastName, maternalLastName) => {
    return http.get('/teachers/preview-email', {
      params: { firstName, paternalLastName, maternalLastName },
    });
  },
};

export default teacherService;
