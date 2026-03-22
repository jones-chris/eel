import { apiBaseUrl, userName, password } from '../../constants/constants.js';

let flowId = null;
const state = {
    flowExecutions: []
};

// Helper function to get query parameters from URL
function getQueryParameter(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

// Fetch all flow executions for a given flow
async function fetchFlowExecutions() {
    try {
        // Get flow ID from URL query parameters
        flowId = getQueryParameter('flowId');

        // Validate that flow ID is present
        if (!flowId) {
            showError('Missing required parameter: flowId');
            return;
        }

        // Show loading spinner
        showLoading();

        // Make the API call to fetch flow executions
        const response = await fetch(
            `${apiBaseUrl}/flow/execution/list?flowId=${encodeURIComponent(flowId)}`,
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
        console.log('Flow executions data:', JSON.stringify(data));

        state.flowExecutions = data;
        hideLoading();
        renderExecutionsTable(state.flowExecutions);

    } catch (error) {
        console.error('Error fetching flow executions:', error);
        hideLoading();
        showError(`Failed to retrieve execution history: ${error.message}`);
    }
}

// Render the flow executions table
function renderExecutionsTable(flowExecutions) {
    const tableBody = document.getElementById('executionsTableBody');

    // Clear existing rows
    tableBody.innerHTML = '';

    // Create a row for each execution
    flowExecutions.forEach(execution => {
        const row = document.createElement('tr');

        const executionTimestamp = formatDateTime(execution.executionTimeStamp);
        const status = execution.status;
        const statusBadgeClass = getStatusBadgeClass(status);
        const workbookBucket = execution.workbookBucket || 'N/A';
        const workbookKey = execution.workbookKey || 'N/A';

        row.innerHTML = `
            <td>${executionTimestamp}</td>
            <td><span class="badge ${statusBadgeClass}">${status}</span></td>
            <td><small>${workbookBucket}</small></td>
            <td><small>${workbookKey}</small></td>
            <td>
                <button class="btn btn-sm btn-info view-details-btn"
                        data-flow-id="${execution.flowId}"
                        data-execution-timestamp="${execution.executionTimeStamp}">
                    📋 View Details
                </button>
            </td>
        `;

        tableBody.appendChild(row);
    });

    // Add click handlers to view details buttons
    document.querySelectorAll('.view-details-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const fId = this.getAttribute('data-flow-id');
            const execTimestamp = this.getAttribute('data-execution-timestamp');
            navigateToExecutionDetails(fId, execTimestamp);
        });
    });

    // Show the table
    document.getElementById('executionsTableContainer').hidden = false;
}

// Navigate to the flow execution details page
function navigateToExecutionDetails(flowId, executionTimestamp) {
    window.location.href = `../flow-executions/flow-executions.html?flowId=${encodeURIComponent(flowId)}&executionTimestamp=${encodeURIComponent(executionTimestamp)}`;
}

// Get Bootstrap badge class based on status
function getStatusBadgeClass(status) {
    switch (status) {
        case 'COMPLETED':
            return 'badge-success';
        case 'FAILED':
            return 'badge-danger';
        case 'RUNNING':
            return 'badge-warning';
        default:
            return 'badge-primary';
    }
}

// Show loading spinner
function showLoading() {
    document.getElementById('loadingSpinner').hidden = false;
    document.getElementById('executionsTableContainer').hidden = true;
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
    document.getElementById('executionsTableContainer').hidden = true;
    document.getElementById('noDataMessage').hidden = true;
}

// Show no data message
function showNoData() {
    document.getElementById('noDataMessage').hidden = false;
    document.getElementById('executionsTableContainer').hidden = true;
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
    console.log('Refreshing flow executions...');
    fetchFlowExecutions();
});

// Handle back button click - return to list-flows
document.getElementById('backButton').addEventListener('click', function() {
    window.location.href = '../list-flows/list-flows.html';
});

// Load flow executions when page loads
window.onload = function() {
    console.log('Page loaded, fetching flow executions...');
    fetchFlowExecutions();
}

