class OutputDestination extends HTMLElement {

    name;

    columnNames;

    columnDateTypes;

    #availableDestinationDataSourceNames = [];

    destinationDataSourceName = null;

    destinationDataSource;

    #destinationService;

    parameterElement;

    render() {
        const parameterElementInstance = this.#renderDestinationParameters();
        const parameterPlaceholderHTML = parameterElementInstance ?
                                         parameterElementInstance.html() :
                                         '';

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
                        Object.keys(this.#availableDestinationDataSourceNames).map(destination => {
                            return `<option 
                                        value="${destination}"
                                        ${(this.destinationDataSourceName === destination) ? "selected" : ""}
                                    >
                                        ${this.#availableDestinationDataSourceNames[destination].displayText}
                                    </option>`
                        }).join('')
                    }
                </select>
            </div>

            ${parameterPlaceholderHTML}
        </div> `;

        if (! this.shadowRoot) {
            this.attachShadow({ mode: 'open' });
            this.shadowRoot.appendChild(template.content.cloneNode(true));
        } else {
            this.shadowRoot.getElementById(this.name).innerHTML = template.innerHTML;
        }

        if (parameterElementInstance) {
            this.shadowRoot.getElementById(this.name).appendChild(parameterElementInstance);
            parameterElementInstance.registerHandlers(this.shadowRoot);
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

        this.render();
    }

    #renderDestinationParameters() {
        if (this.destinationDataSourceName) {
            if (this.destinationDataSourceName === 'email') {
                let destination = new EmailDestination(this);
                this.parameterElement = destination;

                return destination;
            } else if (this.destinationDataSourceName === 'sms') {
                let destination = new SmsDestination(this);
                this.parameterElement = destination;

                return destination;
            } else if (this.destinationDataSourceName === 'file-storage') {
                let destination = new FileStorageDestination(this);
                this.parameterElement = destination;

                return destination;
            } else if (this.destinationDataSourceName === 'db-write') {
                let destination = new DatabaseWriteDestination(this);
                this.parameterElement = destination;

                return destination;
            } else if (this.destinationDataSourceName === 'queue-write') {
                let destination = new QueueWriteDestination(this);
                this.parameterElement = destination;

                return destination;
            } else if (this.destinationDataSourceName === 'trigger-flow') {
                let destination = new TriggerFlowDestination(this);
                this.parameterElement = destination;

                return destination;
            } else {
                const message = `Did not recognize destination data source name of ${this.destinationDataSourceName}`;
                console.error(message);

                throw new Error(message);
            }
        } else {
            return null;
        }
    }

}

customElements.define('output-destination', OutputDestination);