class DatabaseWriteDestination extends HTMLElement {

    #name;

    #parameters = {
        dataSource: null
    }

    html() {
        return `
            <div>
                <div class="input-group mb-3">
                   <div class="input-group-prepend">
                       <label class="input-group-text">Data Sources</label>
                   </div>
                   <select id="dataSource_${this.name}" class="form-select">
                       <option value="">Choose...</option>
                       <option value="1">One</option>
                       <option value="2">Two</option>
                       <option value="3">Three</option>
                   </select>
               </div>
            </div>
        `;
    }

    constructor(outputDestination) {
        super();

        this.#name = `databaseWrite_${outputDestination?.name}`;
    }

    registerHandlers(shadowRoot) {
        shadowRoot.getElementById(`dataSource_${this.name}`)
            .addEventListener('change', this.handleDataSourceChange.bind(this));
    }

    getParameters() {
        return this.#parameters;
    }

    handleDataSourceChange(event) {
        const newDataSource = (event.target.value === '') ? null : event.target.value;
        this.#parameters.dataSource = newDataSource;
    }

}

customElements.define('database-write-destination', DatabaseWriteDestination);