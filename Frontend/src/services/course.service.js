import apiClient from './http-common';

/**
 * Service to handle course and curriculum API requests.
 * Layer: Frontend Service.
 * Corresponds to Epic 3.
 */
class CourseDataService {
  /**
   * Retrieves all courses for a study plan, optionally filtered by semester.
   * @param {number|string} studyPlanId
   * @param {number} [semester] optional semester filter (1 to 4)
   */
  getCoursesByStudyPlan(studyPlanId, semester) {
    const url = semester
      ? `/courses?studyPlanId=${studyPlanId}&semester=${semester}`
      : `/courses?studyPlanId=${studyPlanId}`;
    return apiClient.get(url);
  }

  /**
   * Retrieves a course by ID.
   * @param {number|string} id
   */
  getCourseById(id) {
    return apiClient.get(`/courses/${id}`);
  }

  /**
   * Registers a new course within a study plan.
   * @param {Object} data - CourseRegistrationDTO
   */
  createCourse(data) {
    return apiClient.post('/courses', data);
  }

  /**
   * Updates an existing course.
   * @param {number|string} id
   * @param {Object} data - CourseUpdateDTO
   */
  updateCourse(id, data) {
    return apiClient.put(`/courses/${id}`, data);
  }

  /**
   * Deletes a course.
   * @param {number|string} id
   */
  deleteCourse(id) {
    return apiClient.delete(`/courses/${id}`);
  }
}

export default new CourseDataService();
