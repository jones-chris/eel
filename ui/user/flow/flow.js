import { apiBaseUrl, userName, password } from '../../constants/constants.js';

let flowId = null;
let flowVersion = null;
const state = {
    flow: null
};

// Helper function to get query parameters from URL
function getQueryParameter(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

// Fetch flow details
async function fetchFlowDetails() {
    try {
        // Get flow ID and version from URL query parameters
        flowId = getQueryParameter('id');
        flowVersion = getQueryParameter('version');

        // Validate that both parameters are present
        if (!flowId || !flowVersion) {
            showError('Missing required parameters: id and version');
            return;
        }

        // Show loading spinner
        showLoading();

        // Make the API call to fetch flow details
        const response = await fetch(
            `${apiBaseUrl}/flow?id=${encodeURIComponent(flowId)}&version=${encodeURIComponent(flowVersion)}`,
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
        console.log('Flow data:', JSON.stringify(data));

        state.flow = data;
        hideLoading();
        renderFlowDetails(state.flow);

    } catch (error) {
        console.error('Error fetching flow details:', error);
        hideLoading();
        showError(`Failed to retrieve flow details: ${error.message}`);
    }
}

// Render the flow details on the page
function renderFlowDetails(flow) {
    // Populate basic flow information
    document.getElementById('flowId').textContent = flow.id;
    document.getElementById('version').textContent = flow.version;
    document.getElementById('author').textContent = flow.author || 'N/A';
    document.getElementById('inputType').textContent = flow.inputType || 'N/A';

    // Set status badge with appropriate styling
    const statusElement = document.getElementById('status');
    const isFinalized = flow.finalized;
    statusElement.textContent = isFinalized ? 'DEPLOYED' : 'DRAFT';

    // Apply Bootstrap badge styling based on status
    statusElement.classList.remove('badge-success', 'badge-warning');
    if (isFinalized) {
        statusElement.classList.add('badge-success');
    } else {
        statusElement.classList.add('badge-warning');
    }

    // Render scheduled batch configuration if present
    if (flow.scheduledBatchConfiguration) {
        renderScheduledBatchConfiguration(flow.scheduledBatchConfiguration);
    }

    // Render streaming configuration if present
    if (flow.streamingConfiguration) {
        renderStreamingConfiguration(flow.streamingConfiguration);
    }

    // Render output configuration if present
    if (flow.outputConfiguration) {
        renderOutputConfiguration(flow.outputConfiguration);
    }

    // Show the flow details card
    document.getElementById('flowDetailsCard').hidden = false;
}

// Render scheduled batch configuration
function renderScheduledBatchConfiguration(config) {
    document.getElementById('cronExpression').textContent = config.cronExpression || 'N/A';

    const sheetQueriesContainer = document.getElementById('sheetQueriesContainer');
    sheetQueriesContainer.innerHTML = '';

    if (config.sheetQueries && Object.keys(config.sheetQueries).length > 0) {
        for (const [sheetName, queryData] of Object.entries(config.sheetQueries)) {
            const queryDiv = document.createElement('div');
            queryDiv.className = 'mb-3';
            queryDiv.innerHTML = `
                <strong>${sheetName}:</strong><br>
                <small class="text-muted">SQL: ${queryData.sql || 'N/A'}</small><br>
                <small class="text-muted">Data Source: ${queryData.dataSource || 'N/A'}</small>
            `;
            sheetQueriesContainer.appendChild(queryDiv);
        }
    } else {
        sheetQueriesContainer.innerHTML = '<small class="text-muted">No sheet queries configured</small>';
    }

    document.getElementById('scheduledBatchSection').hidden = false;
}

// Render streaming configuration
function renderStreamingConfiguration(config) {
    // For now, just show a placeholder. In a real implementation, you'd parse the streaming config structure
    document.getElementById('streamDetails').textContent = 'Streaming configuration details would be displayed here';
    document.getElementById('streamingSection').hidden = false;
}

// Render output configuration
function renderOutputConfiguration(config) {
    // For now, just show a placeholder. In a real implementation, you'd parse the output config structure
    document.getElementById('outputDetails').textContent = 'Output configuration details would be displayed here';
    document.getElementById('outputSection').hidden = false;
}

// Show loading spinner
function showLoading() {
    document.getElementById('loadingSpinner').hidden = false;
    document.getElementById('flowDetailsCard').hidden = true;
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
    document.getElementById('flowDetailsCard').hidden = true;
    document.getElementById('noDataMessage').hidden = true;
}

// Show no data message
function showNoData() {
    document.getElementById('noDataMessage').hidden = false;
    document.getElementById('flowDetailsCard').hidden = true;
    document.getElementById('errorMessage').hidden = true;
}

// Handle refresh button click
document.getElementById('refreshButton').addEventListener('click', function() {
    console.log('Refreshing flow details...');
    fetchFlowDetails();
});

// Handle view executions button click
document.getElementById('viewExecutionsButton').addEventListener('click', function() {
    if (flowId) {
        window.location.href = `../list-flow-executions/list-flow-executions.html?flowId=${encodeURIComponent(flowId)}`;
    } else {
        alert('Flow ID is not available');
    }
});

// Handle back button click - return to list-flows
document.getElementById('backButton').addEventListener('click', function() {
    window.location.href = '../list-flows/list-flows.html';
});

// Load flow details when page loads
window.onload = function() {
    console.log('Page loaded, fetching flow details...');
    fetchFlowDetails();
}

