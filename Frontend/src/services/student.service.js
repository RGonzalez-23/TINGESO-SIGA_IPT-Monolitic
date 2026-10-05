import apiClient from './http-common';

/**
 * Service to handle all student-related API requests.
 * Layer: Frontend Service.
 */
class StudentDataService {
  /**
   * Retrieves all students.
   */
  getAll() {
    return apiClient.get('/students');
  }

  /**
   * Retrieves a student by their RUN.
   * @param {string} run
   */
  getByRun(run) {
    return apiClient.get(`/students/${run}`);
  }

  /**
   * Registers a new student.
   * @param {Object} data { run, firstName, paternalLastName, maternalLastName, careerCode }
   */
  create(data) {
    return apiClient.post('/students', data);
  }

  /**
   * Updates student information.
   * @param {string} run
   * @param {Object} data { firstName, paternalLastName, maternalLastName, careerCode, academicStatus }
   */
  update(run, data) {
    return apiClient.put(`/students/${run}`, data);
  }

  /**
   * Deletes a student physically (only if no enrollments or history exist).
   * @param {string} run
   */
  delete(run) {
    return apiClient.delete(`/students/${run}`);
  }
}

export default new StudentDataService();
