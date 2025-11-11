class OutputDestination extends HTMLElement {

    name;

    columnNames;

    columnDateTypes;

    #availableDestinationDataSourceNames = [];

    destinationDataSourceName = null;

    destinationDataSource = {};

    #destinationService;

    render() {
        const template = document.createElement('template');
        template.innerHTML = `
        <div id="${this.name}" class="card" style="width: 18rem;">
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
                    <label class="input-group-text">Destination</label>
                </div>
                <select id="destinationDataSource-${this.name}" class="form-select">
                    <option value="">Choose...</option>
                    ${
                        this.#availableDestinationDataSourceNames.map(destination => {
                            return `<option 
                                        value="${destination}"
                                        ${(this.destinationDataSourceName === destination) ? "selected" : ""}
                                    >
                                        ${destination}
                                    </option>`
                        }).join('')
                    }
                </select>
            </div>

            ${this.#renderDestinationParameters()}
        </div> `;

        if (! this.shadowRoot) {
            this.attachShadow({ mode: 'open' });
            this.shadowRoot.appendChild(template.content.cloneNode(true));
        } else {
            this.shadowRoot.getElementById(this.name).innerHTML = template.innerHTML;
        }

        // Register handlers.
        const selectElement = this.shadowRoot.getElementById(`destinationDataSource-${this.name}`);
        selectElement.addEventListener('change', this.handleDestinationDataSourceNameChange.bind(this));  
    }

    constructor(outputSheetMetadata, destinationService) {
        super();

        this.name = outputSheetMetadata?.name;
        this.columnNames = outputSheetMetadata?.columnNames;
        this.columnDataTypes = outputSheetMetadata?.columnDataTypes
        this.#destinationService = destinationService;
    }

    async connectedCallback() {
        // Service calls that are required to hydrate the component before rendering it.
        let destinations = await this.#destinationService.getDataSources();
        this.#availableDestinationDataSourceNames = destinations; 

        // Render component.
        this.render();  
    }

    async handleDestinationDataSourceNameChange(event) {
        const newDestinationDataSourceName = event.target.value;
        this.destinationDataSourceName = newDestinationDataSourceName;

        if (this.destinationDataSourceName === "") {
            this.handleDestinationDataSourceChange({});
            return;
        }

        let destinationDataSource = await this.#destinationService.getDataSourceByName(newDestinationDataSourceName);
        this.handleDestinationDataSourceChange(destinationDataSource);
    }

    handleDestinationDataSourceChange(newDestinationDataSource) {
        this.destinationDataSource = newDestinationDataSource;

        this.render();
    }

    #renderDestinationParameters() {
        if (this.destinationDataSource.metadata) {
            return '<p>Hello World!</p>'
        } else {
            return '<p>Go away world!</p>'
        }
    }

}

customElements.define('output-destination', OutputDestination);