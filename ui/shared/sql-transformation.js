class SqlTransformation extends HTMLElement {

    sql;

    render() {
        return `
            <div>
                <textarea id="sqlTransformationScript"></textarea>
            </div>
        `;
    }

    constructor(sql = null) {
        super();

        this.sql = sql;
    }

    connectedCallback() {
        const template = document.createElement('template');
        template.innerHTML = this.render();

        this.attachShadow({ mode: 'open' });
        this.shadowRoot.appendChild(template.content.cloneNode(true));

        // Attach state handlers.
        const sqlTextArea = this.shadowRoot.getElementById(`sqlTransformationScript`);
        sqlTextArea.addEventListener('input', this.handleSqlChange.bind(this));
    }

    handleSqlChange(event) {
        const newSql = event.target.value;
        this.sql = newSql;
    }

}

customElements.define("sql-transformation", SqlTransformation);