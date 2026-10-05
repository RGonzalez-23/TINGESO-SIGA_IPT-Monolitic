import apiClient from './http-common';

/**
 * Service to manage user accounts and security settings.
 */
const changePassword = (username, newPassword, temporary = false) => {
  return apiClient.put(`/users/${encodeURIComponent(username)}/password?temporary=${temporary}`, {
    newPassword,
  });
};

const UserService = {
  changePassword,
};

export default UserService;
