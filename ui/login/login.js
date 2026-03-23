import { apiBaseUrl } from '../constants/constants.js';

// Check if user is authenticated
function isAuthenticated() {
    const jwt = getStoredJWT();
    if (!jwt) return false;

    try {
        // Basic JWT validation - check if not expired
        const payload = JSON.parse(atob(jwt.split('.')[1]));
        const currentTime = Date.now() / 1000;
        return payload.exp > currentTime;
    } catch (error) {
        console.error('Invalid JWT:', error);
        clearJWT();
        return false;
    }
}

// Login function
async function login(username, password) {
    try {
        // Show loading state
        showLoading();

        // Call the auth endpoint
        const response = await fetch(`${apiBaseUrl}/auth`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Basic ' + btoa(username + ':' + password)
            }
        });

        if (!response.ok) {
            throw new Error(`Authentication failed: ${response.status}`);
        }

        const data = await response.json();
        const jwt = data.token || data.jwt || data.access_token;

        if (!jwt) {
            throw new Error('No JWT token received from server');
        }

        // Store the JWT
        storeJWT(jwt);

        console.log('Login successful, JWT stored');

        // Redirect to main application
        window.location.href = './user/list-flows/list-flows.html';

    } catch (error) {
        console.error('Login error:', error);
        showError(error.message);
    } finally {
        hideLoading();
    }
}

// Show loading spinner
function showLoading() {
    document.getElementById('loadingSpinner').hidden = false;
    document.getElementById('errorMessage').hidden = true;
    document.getElementById('loginButton').disabled = true;
    document.getElementById('loginForm').querySelectorAll('input').forEach(input => {
        input.disabled = true;
    });
}

// Hide loading spinner
function hideLoading() {
    document.getElementById('loadingSpinner').hidden = true;
    document.getElementById('loginButton').disabled = false;
    document.getElementById('loginForm').querySelectorAll('input').forEach(input => {
        input.disabled = false;
    });
}

// Show error message
function showError(errorText) {
    document.getElementById('errorMessage').hidden = false;
    document.getElementById('errorText').textContent = errorText;
}

// Handle form submission
document.getElementById('loginForm').addEventListener('submit', function(event) {
    event.preventDefault();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showError('Please enter both username and password');
        return;
    }

    login(username, password);
});

// Check if already authenticated on page load
window.onload = function() {
    if (isAuthenticated()) {
        console.log('User already authenticated, redirecting...');
        window.location.href = './user/list-flows/list-flows.html';
    }
}