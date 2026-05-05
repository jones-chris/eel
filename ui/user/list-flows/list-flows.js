import { apiBaseUrl } from '../../constants/constants.js';
import { getAuthHeader, getUserName } from '../../utils/auth.js';

const state = {
    flowIds: [],
    expandedFlows: new Map() // flowId -> {versions: [], loading: boolean, error: string}
};

// Fetch all flows for the current user
async function fetchFlows() {
    try {
        // Show loading spinner
        showLoading();

        // Make the API call to fetch flows
        const response = await fetch(
            `${apiBaseUrl}/flow/list?userName=${getUserName()}`,
            {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: getAuthHeader()
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
        console.log('Flows data:', JSON.stringify(data));

        state.flowIds = data.flowIds || [];
        hideLoading();
        renderFlowsTable(state.flowIds);
    } catch (error) {
        console.error('Error fetching flows:', error);
        hideLoading();
        showError(`Failed to retrieve flows: ${error.message}`);
    }
}

// Render the flows table
function renderFlowsTable(flowIds) {
    const tableBody = document.getElementById('flowsTableBody');

    // Clear existing rows
    tableBody.innerHTML = '';

    // Create a row for each flow
    flowIds.forEach(flowId => {
        const row = document.createElement('tr');
        row.className = 'flow-row';
        row.setAttribute('data-flow-id', flowId);

        const expandedData = state.expandedFlows.get(flowId);
        const isExpanded = expandedData && !expandedData.loading;

        row.innerHTML = `
            <td>
                <span class="expand-icon">${isExpanded ? '▼' : '▶'}</span>
                <code>${flowId}</code>
            </td>
            <td>
                <small class="text-muted">
                    ${expandedData ? (expandedData.loading ? 'Loading versions...' : `${expandedData.versions.length} version(s)`) : 'Click to expand'}
                </small>
            </td>
        `;

        // Add click event listener to toggle expansion
        row.addEventListener('click', () => toggleFlowExpansion(flowId));

        tableBody.appendChild(row);

        // If expanded, add version rows
        if (isExpanded) {
            if (expandedData.error) {
                const errorRow = document.createElement('tr');
                errorRow.className = 'version-row';
                errorRow.innerHTML = `
                    <td colspan="2" class="text-danger">
                        <small>Error loading versions: ${expandedData.error}</small>
                    </td>
                `;
                tableBody.appendChild(errorRow);
            } else {
                expandedData.versions.forEach(version => {
                    const versionRow = document.createElement('tr');
                    versionRow.className = 'version-row';
                    versionRow.innerHTML = `
                        <td class="version-cell">
                            <small>Version ${version}</small>
                        </td>
                        <td>
                            <a href="../flow/flow.html?id=${encodeURIComponent(flowId)}&version=${encodeURIComponent(version)}"
                               class="btn btn-sm btn-outline-primary view-version-btn">
                                📋 View Details
                            </a>
                        </td>
                    `;
                    tableBody.appendChild(versionRow);
                });
            }
        }
    });

    // Show the table
    document.getElementById('flowsTableContainer').hidden = false;
}

// Toggle flow expansion to show/hide versions
async function toggleFlowExpansion(flowId) {
    const expandedData = state.expandedFlows.get(flowId);

    if (expandedData && !expandedData.loading) {
        // Already expanded, collapse it
        state.expandedFlows.delete(flowId);
        renderFlowsTable(state.flowIds);
        return;
    }

    // Not expanded, fetch versions
    state.expandedFlows.set(flowId, { versions: [], loading: true, error: null });
    renderFlowsTable(state.flowIds);

    try {
        const versions = await fetchFlowVersions(flowId);
        state.expandedFlows.set(flowId, { versions, loading: false, error: null });
    } catch (error) {
        state.expandedFlows.set(flowId, { versions: [], loading: false, error: error.message });
    }

    renderFlowsTable(state.flowIds);
}

// Fetch all available versions for a flow
async function fetchFlowVersions(flowId) {
    try {
        const response = await fetch(
            `${apiBaseUrl}/flow/versions?id=${flowId}`,
            {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: getAuthHeader()
                }
            }
        );

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log(`Versions for flow ${flowId}:`, JSON.stringify(data));

        return data['flowVersions'] || [];
    } catch (error) {
        console.error(`Error fetching version ${version} for flow ${flowId}:`, error);
        throw error;
    }
}

// Show loading spinner
function showLoading() {
    document.getElementById('loadingSpinner').hidden = false;
    document.getElementById('flowsTableContainer').hidden = true;
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
    document.getElementById('flowsTableContainer').hidden = true;
    document.getElementById('noDataMessage').hidden = true;
}

// Show no data message
function showNoData() {
    document.getElementById('noDataMessage').hidden = false;
    document.getElementById('flowsTableContainer').hidden = true;
    document.getElementById('errorMessage').hidden = true;
}

// Handle refresh button click
document.getElementById('refreshButton').addEventListener('click', function() {
    console.log('Refreshing flows...');
    fetchFlows();
});

// Handle create flow button click
document.getElementById('createFlowButton').addEventListener('click', function() {
    window.location.href = '../create-flow/create-flow.html';
});

// Load flows when page loads
window.onload = function() {
    console.log('Page loaded, fetching flows...');
    fetchFlows();
}
