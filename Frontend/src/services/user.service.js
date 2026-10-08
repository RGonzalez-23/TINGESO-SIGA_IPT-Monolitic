import apiClient from './http-common';

/**
 * Service to manage user accounts and security settings.
 * Supports administrator resets (without currentPassword) and self-service changes (with currentPassword).
 */
const changePassword = (username, newPassword, temporary = false, currentPassword = null) => {
  const payload = { newPassword };
  if (currentPassword) {
    payload.currentPassword = currentPassword;
  }
  return apiClient.put(`/users/${encodeURIComponent(username)}/password?temporary=${temporary}`, payload);
};

const UserService = {
  changePassword,
};

export default UserService;
