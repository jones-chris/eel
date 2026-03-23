// JWT storage key
const JWT_STORAGE_KEY = 'anyeasel_jwt';

// Get stored JWT
export function getStoredJWT() {
    return localStorage.getItem(JWT_STORAGE_KEY);
}

// Store JWT securely
export function storeJWT(jwt) {
    localStorage.setItem(JWT_STORAGE_KEY, jwt);
}

// Clear stored JWT (logout)
export function clearJWT() {
    localStorage.removeItem(JWT_STORAGE_KEY);
}

// Get authorization header - checks for JWT first, falls back to basic auth
export function getAuthHeader() {
    const jwt = getStoredJWT();
    if (jwt) {
        return `Bearer ${jwt}`;
    }

    // Fallback to basic auth for development/testing
    const username = null; // todo: parameterize this
    const password = null; // todo: parameterize this
    return "Basic " + btoa(username + ":" + password);
}