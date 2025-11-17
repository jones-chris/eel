class TriggerFlowDestination extends HTMLElement {

    #name;

    #parameters = {
        flowId: null,
        flowVersion: null,
        flowInputMapping: null
    }

    html() {
        return `
            <div>
                <div class="input-group mb-3">
                   <div class="input-group-prepend">
                       <label class="input-group-text">Flows</label>
                   </div>
                   <select id="flow_${this.name}" class="form-select">
                       <option value="">Choose...</option>
                       <option value="1">One</option>
                       <option value="2">Two</option>
                       <option value="3">Three</option>
                   </select>
                </div>

               <div class="input-group mb-3">
                  <div class="input-group-prepend">
                      <label class="input-group-text">Versions</label>
                  </div>
                  <select id="version_${this.name}" class="form-select">
                      <option value="">Choose...</option>
                      <option value="1">One</option>
                      <option value="2">Two</option>
                      <option value="3">Three</option>
                  </select>
               </div>

              <div class="input-group mb-3">
                 <div class="input-group-prepend">
                     <label class="input-group-text">Output-to-Input Mapping</label>
                 </div>
                 <select id="input_${this.name}" class="form-select">
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

        this.#name = `triggerFlow_${outputDestination?.name}`;
    }

    registerHandlers(shadowRoot) {
        shadowRoot.getElementById(`flow_${this.name}`)
            .addEventListener('change', this.handleFlowIdChange.bind(this));

        shadowRoot.getElementById(`version_${this.name}`)
            .addEventListener('change', this.handleFlowVersionChange.bind(this));

        shadowRoot.getElementById(`input_${this.name}`)
            .addEventListener('change', this.handleFlowInputMappingChange.bind(this));
    }

    getParameters() {
        return this.#parameters;
    }

    handleFlowIdChange(event) {
        const newFlowId = (event.target.value === '') ? null : event.target.value;
        this.#parameters.flowId = newFlowId;
    }

    handleFlowVersionChange(event) {
        const newFlowVersion = (event.target.value === '') ? null : event.target.value;
        this.#parameters.flowVersion = newFlowVersion;
    }

    handleFlowInputMappingChange(event) {
        const newInputMappingChange = (event.target.value === '') ? null : event.target.value;
        this.#parameters.flowInputMapping = newInputMappingChange;
    }

}

customElements.define('trigger-flow-destination', TriggerFlowDestination);