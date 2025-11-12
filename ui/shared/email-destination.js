class EmailDestination extends HTMLElement {
 
    name;

    recipients = [];

    subject;

    body;

    #outputDestination;

    render() {
        const template = document.createElement('template');
        template.innerHTML = `
            <p id="email_${this.name}">Email Distribution Custom HTML Element</p>
        `;
        

        // this.#outputDestination.parameterElement = this;  
        if (! this.shadowRoot) {
            this.attachShadow({ mode: 'open' });
            this.shadowRoot.appendChild(template.content.cloneNode(true));
        } else {
            this.shadowRoot.getElementById(this.name).innerHTML = template.innerHTML;
        }
    }

    constructor(outputDestination) {
        super();

        this.#outputDestination = outputDestination;
        this.name = `email_${outputDestination?.name}`;
    }

    connectedCallback() {
        this.render();
    }

}


customElements.define('email-destination', EmailDestination);