import apiClient from './http-common';

const enrollmentService = {
  getAvailableOffer(studentRun) {
    const params = studentRun ? { studentRun } : undefined;
    return apiClient.get('/enrollments/available-offer', { params });
  },
  getMyEnrollments() {
    return apiClient.get('/enrollments/my-enrollments');
  },
  getStudentActiveEnrollments(run) {
    return apiClient.get(`/enrollments/student/${run}/active`);
  },
  register(studentRun, sectionIds) {
    return apiClient.post('/enrollments/register', { studentRun, sectionIds });
  },
  cancel(enrollmentId) {
    return apiClient.delete(`/enrollments/${enrollmentId}`);
  },
};

export default enrollmentService;