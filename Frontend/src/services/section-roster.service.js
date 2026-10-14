import apiClient from './http-common';

const sectionRosterService = {
  getMySections() {
    return apiClient.get('/sections/teacher/my-sections');
  },
  getEnrolledStudents(sectionId) {
    return apiClient.get(`/sections/${sectionId}/enrolled-students`);
  },
};

export default sectionRosterService;