import { apiBaseUrl, userName, password } from '../../constants/constants.js';

let flowId = null;
let executionTimestamp = null;
const state = {
    flowExecution: null
};

// Helper function to get query parameters from URL
function getQueryParameter(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

// Fetch flow execution status
async function fetchFlowExecutionStatus() {
    try {
        // Get flow ID and execution timestamp from URL query parameters
        flowId = getQueryParameter('flowId');
        executionTimestamp = getQueryParameter('executionTimestamp');

        // Validate that both parameters are present
        if (!flowId || !executionTimestamp) {
            const errorMessage = 'Missing required parameters: flowId and executionTimestamp';

            console.error(errorMessage)
            showError(errorMessage);

            return;
        }

        // Show loading spinner
        showLoading();

        // Make the API call to fetch execution status
        const response = await fetch(
            `${apiBaseUrl}/flow/execution/status?flowId=${encodeURIComponent(flowId)}&executionTimestamp=${encodeURIComponent(executionTimestamp)}`,
            {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: "Basic " + btoa(userName + ":" + password)
                }
            }
        );

        if (response.status === 404) {
            hideLoading();
            showNoData();
            return;
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('Flow execution data:', JSON.stringify(data));

        state.flowExecution = data;
        hideLoading();
        renderExecutionDetails(state.flowExecution);
    } catch (error) {
        console.error('Error fetching flow execution status:', error);
        hideLoading();
        showError(`Failed to retrieve execution status: ${error.message}`);
    }
}

// Render the execution details on the page
function renderExecutionDetails(flowExecution) {
    // Populate execution details
    document.getElementById('flowId').textContent = flowExecution.flowId;
    document.getElementById('executionTimestamp').textContent = formatDateTime(flowExecution.executionTimeStamp);
    document.getElementById('workbookBucket').textContent = flowExecution.workbookBucket || 'N/A';
    document.getElementById('workbookKey').textContent = flowExecution.workbookKey || 'N/A';

    // Set status badge with appropriate styling
    const statusElement = document.getElementById('status');
    const status = flowExecution.status;
    statusElement.textContent = status;

    // Apply Bootstrap badge styling based on status
    statusElement.classList.remove('badge-primary', 'badge-success', 'badge-danger', 'badge-warning');
    if (status === 'COMPLETED') {
        statusElement.classList.add('badge-success');
    } else if (status === 'FAILED') {
        statusElement.classList.add('badge-danger');
    } else if (status === 'RUNNING') {
        statusElement.classList.add('badge-warning');
    } else {
        statusElement.classList.add('badge-primary');
    }

    // Show the execution details card
    document.getElementById('executionDetailsCard').hidden = false;
}

// Show loading spinner
function showLoading() {
    document.getElementById('loadingSpinner').hidden = false;
    document.getElementById('executionDetailsCard').hidden = true;
    document.getElementById('errorMessage').hidden = true;
    document.getElementById('noDataMessage').hidden = true;
}

// Hide loading spinner
function hideLoading() {
    document.getElementById('loadingSpinner').hidden = true;
}

// Show error message
function showError(errorText) {
    document.getElementById('errorMessage').hidden = false;
    document.getElementById('errorText').textContent = errorText;
    document.getElementById('executionDetailsCard').hidden = true;
    document.getElementById('noDataMessage').hidden = true;
}

// Show no data message
function showNoData() {
    document.getElementById('noDataMessage').hidden = false;
    document.getElementById('executionDetailsCard').hidden = true;
    document.getElementById('errorMessage').hidden = true;
}

// Format ISO datetime to readable format
function formatDateTime(isoDateTime) {
    if (!isoDateTime) return 'N/A';
    try {
        const date = new Date(isoDateTime);
        return date.toLocaleString();
    } catch (error) {
        return isoDateTime;
    }
}

// Handle refresh button click
document.getElementById('refreshButton').addEventListener('click', function() {
    console.log('Refreshing execution status...');
    fetchFlowExecutionStatus();
});

// Handle view all executions button click
document.getElementById('viewAllExecutionsButton').addEventListener('click', function() {
    if (flowId) {
        window.location.href = `../list-flow-executions/list-flow-executions.html?flowId=${encodeURIComponent(flowId)}`;
    } else {
        alert('Flow ID is not available');
    }
});

// Handle back button click - return to list-flows or user landing
document.getElementById('backButton').addEventListener('click', function() {
    window.location.href = '../list-flows/list-flows.html';
});

// Load execution status when page loads
window.onload = function() {
    console.log('Page loaded, fetching flow execution status...');
    fetchFlowExecutionStatus();
}

