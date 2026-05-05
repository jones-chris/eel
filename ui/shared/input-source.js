class InputSource extends HTMLElement {

    name;

    columnNames = [];

    columnDataTypes = {};

    dataSource = {
        id: null,
        name: null
    };

    sql = "";

    render() {
        return `
            <div class="card" style="width: 18rem;">
                <div class="card-body">
                    <h5 class="card-title">${this.name}</h5>
                </div>

                <div class="card-body">
                    Columns
                </div>
                <ol class="list-group list-group-flush">
                    ${
                        this.columnNames.map(columnName => {
                            let columnDataType = this.columnDataTypes[columnName];
                            return `<li class="list-group-item">${columnName} (${columnDataType})</li>`
                        }).join('')
                    }
                </ol>

                <div class="input-group mb-3">
                    <div class="input-group-prepend">
                        <label class="input-group-text">Data Sources</label>
                    </div>
                    <select id="dataSource-${this.name}" class="form-select">
                        <option value="e70516d6-1246-4e65-b710-e653c00f10fa">Finance DB</option>
                        <option value="fe559887-0791-49e8-a3f8-56d8c717d333">HR DB</option>
                        <option value="836c2d22-c109-438a-a4b5-6c9935be578f">Customer DB</option>
                    </select>
                </div>

                <div>
                    <label for="sql-${this.name}">SQL Query</label>
                    <div>
                        <textarea id="sql-${this.name}" ></textarea>
                    </div>
                </div>
            </div>`;
    }

    constructor(inputSheetMetadata) {
        super();

        this.name = inputSheetMetadata?.name;
        this.columnNames = inputSheetMetadata?.columnNames;
        this.columnDataTypes = inputSheetMetadata?.columnDataTypes;
    }

    connectedCallback() {
        const template = document.createElement('template');
        template.innerHTML = this.render();

        this.attachShadow({ mode: 'open' });
        this.shadowRoot.appendChild(template.content.cloneNode(true));

        // Attach state handlers.
        // 1. DataSource
        const selectElement = this.shadowRoot.getElementById(`dataSource-${this.name}`);
        selectElement.addEventListener('change', this.handleDataSourceChange.bind(this));

        // 2. SQL
        const sqlTextarea = this.shadowRoot.getElementById(`sql-${this.name}`);
        sqlTextarea.addEventListener('input', this.handleSqlChange.bind(this));
    }

    handleDataSourceChange(event) {
        const dataSourceUuid = event.target.value;
        const dataSourceName = event.target.options[event.target.selectedIndex].label;

        this.dataSource = {
            id: dataSourceUuid,
            name: dataSourceName
        };
    }

    handleSqlChange(event) {
        const newSql = event.target.value;
        this.sql = newSql;
    }

}

customElements.define("input-source", InputSource);