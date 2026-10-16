import http from './http-common';

/**
 * academicHistoryService manages student transcript retrieval and curriculum progress maps.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
const academicHistoryService = {
  getMyAcademicHistory: () => http.get('/students/me/academic-history'),
  getStudentAcademicHistory: (run) => http.get(`/students/${run}/academic-history`),
  getMyCurriculumProgress: () => http.get('/students/me/curriculum-progress'),
  getStudentCurriculumProgress: (run) => http.get(`/students/${run}/curriculum-progress`),
};

export default academicHistoryService;
