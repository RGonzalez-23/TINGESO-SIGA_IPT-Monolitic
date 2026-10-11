import apiClient from './http-common';

/**
 * Service to handle standardized time blocks (L1 - V6).
 * Layer: Frontend Service.
 */
class TimeBlockService {
  /**
   * Retrieves all 30 standardized schedule blocks.
   */
  getAll() {
    return apiClient.get('/time-blocks');
  }
}

export default new TimeBlockService();
