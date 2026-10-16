import http from './http-common';

/**
 * gradeService manages retrieval and batch submission of final course grades.
 * Corresponds to Epic 7: Final Grades Registration and Semester Close.
 */
const gradeService = {
  getSectionGrades: (sectionId) => http.get(`/sections/${sectionId}/grades`),
  submitSectionGrades: (sectionId, grades) =>
    http.put(`/sections/${sectionId}/grades`, { grades }),
};

export default gradeService;
