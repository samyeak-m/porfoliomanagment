class VarStockDropdown {
  constructor() {
    this.stockInput = document.getElementById("stockSymbol");
    this.dropdownContainer = document.getElementById("stockDropdownList");
    this.stockSymbols = [];
    this.selectedIndex = -1;
    this.init();
  }

  async init() {
    await this.loadStockSymbols();
    this.bindEvents();
  }

  async loadStockSymbols() {
    try {
      const response = await fetch("/api/var/stock-symbols");
      if (response.ok) {
        this.stockSymbols = await response.json();
        console.log("Loaded stock symbols:", this.stockSymbols.length);
      } else {
        console.error("Failed to load stock symbols");
      }
    } catch (error) {
      console.error("Error loading stock symbols:", error);
    }
  }

  bindEvents() {
    this.stockInput.addEventListener("input", (e) => this.handleInput(e));
    this.stockInput.addEventListener("keydown", (e) => this.handleKeydown(e));
    this.stockInput.addEventListener("focus", () => {
      if (this.stockInput.value.trim().length > 0) {
        this.handleInput({ target: this.stockInput });
      }
    });

    this.stockInput.addEventListener("blur", (e) => {
      setTimeout(() => {
        const activeElement = document.activeElement;
        if (
          !this.dropdownContainer.contains(activeElement) &&
          activeElement !== this.stockInput
        ) {
          this.hideDropdown();
        }
      }, 100);
    });

    document.addEventListener("click", (e) => {
      if (
        !this.stockInput.contains(e.target) &&
        !this.dropdownContainer.contains(e.target)
      ) {
        this.hideDropdown();
      }
    });
  }

  handleInput(e) {
    const query = e.target.value.trim().toUpperCase();
    if (query.length === 0) {
      this.hideDropdown();
      return;
    }

    const filteredSymbols = this.stockSymbols
      .filter((symbol) => symbol.toUpperCase().includes(query))
      .slice(0, 10);

    this.renderDropdown(filteredSymbols);
    this.selectedIndex = -1;
  }

  handleKeydown(e) {
    const visibleItems = this.dropdownContainer.querySelectorAll(".dropdown-item");

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        this.selectedIndex = Math.min(this.selectedIndex + 1, visibleItems.length - 1);
        this.updateSelection(visibleItems);
        break;

      case "ArrowUp":
        e.preventDefault();
        this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
        this.updateSelection(visibleItems);
        break;

      case "Enter":
        e.preventDefault();
        if (this.selectedIndex >= 0 && visibleItems[this.selectedIndex]) {
          this.selectSymbol(visibleItems[this.selectedIndex].textContent);
        }
        break;

      case "Escape":
        this.hideDropdown();
        break;
    }
  }

  updateSelection(items) {
    items.forEach((item, index) => {
      item.classList.toggle("selected", index === this.selectedIndex);
    });
  }

  renderDropdown(symbols) {
    if (symbols.length === 0) {
      this.hideDropdown();
      return;
    }

    this.dropdownContainer.innerHTML = symbols
      .map(symbol => `
        <div class="dropdown-item" onclick="varStockDropdown.selectSymbol('${symbol}')" onmousedown="event.preventDefault()">
          <div class="stock-icon">${symbol.substring(0, 2)}</div>
          <span class="stock-symbol">${symbol}</span>
        </div>
      `)
      .join('');

    this.showDropdown();
  }

  selectSymbol(symbol) {
    this.stockInput.value = symbol;
    this.hideDropdown();
    this.stockInput.focus();
    this.stockInput.dispatchEvent(new Event("change"));
  }

  showDropdown() {
    if (this.dropdownContainer.children.length > 0) {
      this.dropdownContainer.style.display = "block";

      const inputRect = this.stockInput.getBoundingClientRect();
      this.dropdownContainer.style.position = "fixed";
      this.dropdownContainer.style.width = inputRect.width + "px";

      const viewportHeight = window.innerHeight;
      const dropdownHeight = 250;

      if (inputRect.bottom + dropdownHeight > viewportHeight - 10) {
        this.dropdownContainer.style.top = inputRect.top - dropdownHeight - 4 + "px";
      }
    }
  }

  hideDropdown() {
    this.dropdownContainer.style.display = "none";
    this.selectedIndex = -1;
  }
}

class VarCalculationForm {
  constructor() {
    this.form = document.getElementById("var-form");
    this.button = document.getElementById("varBtn");
    this.resultsDiv = document.getElementById("varResults");
    this.bindEvents();
  }

  bindEvents() {
    this.form.addEventListener("submit", (e) => this.handleSubmit(e));
  }

  async handleSubmit(e) {
    e.preventDefault();

    const stockSymbol = document.getElementById("stockSymbol").value.trim().toUpperCase();
    
    // Get period value and unit
    const periodValue = parseInt(document.getElementById("periodValue").value);
    const periodUnit = document.getElementById("periodUnit").value;

    // Convert to days
    let daysOfInvestment = periodValue;
    if (periodUnit === "weeks") daysOfInvestment *= 7;
    else if (periodUnit === "months") daysOfInvestment *= 30;
    else if (periodUnit === "years") daysOfInvestment *= 365;

    if (!stockSymbol) {
      this.showError("Please enter a stock symbol");
      return;
    }

    if (periodValue < 1) {
      this.showError("Investment period must be at least 1");
      return;
    }

    if (daysOfInvestment > 3650) { // Max 10 years
      this.showError("Investment period too long. Maximum 10 years allowed.");
      return;
    }

    this.setLoadingState(true);

    try {
      const response = await fetch(`/api/var/calculate/${stockSymbol}?daysOfInvestment=${daysOfInvestment}`, {
        method: "GET",
        headers: { "Content-Type": "application/json" }
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      
      if (data.error) {
        throw new Error(data.error);
      }
      
      this.showResults(data, periodValue, periodUnit, daysOfInvestment);
    } catch (error) {
      this.showError("Unable to calculate VaR. " + error.message);
    } finally {
      this.setLoadingState(false);
    }
  }

  setLoadingState(loading) {
    this.button.disabled = loading;
    this.button.innerHTML = loading
      ? '<div class="var-spinner"></div><span>Calculating...</span>'
      : '<span>Calculate Value at Risk</span>';
  }

  showResults(data, periodValue, periodUnit, daysOfInvestment) {
    const varPercentage = (data.varValue / data.initialPrice) * 100;
    const riskLevel = this.getRiskLevel(varPercentage);
    const riskClass = this.getRiskClass(varPercentage);

    // Format the investment period display
    const periodDisplay = this.formatPeriodDisplay(periodValue, periodUnit, daysOfInvestment);

    this.resultsDiv.innerHTML = `
      <div class="results-header">
        <h3>${data.stockSymbol} VaR Analysis Results</h3>
      </div>
      <div class="results-body">
        <div class="section-title">
          Risk Assessment Summary
        </div>
        
        <div class="var-summary">
          <div class="var-metric">
            <div class="var-metric-label">Stock Symbol</div>
            <div class="var-metric-value">${data.stockSymbol}</div>
          </div>
          <div class="var-metric">
            <div class="var-metric-label">Initial Price</div>
            <div class="var-metric-value">NPR ${data.initialPrice.toFixed(2)}</div>
          </div>
          <div class="var-metric">
            <div class="var-metric-label">Value at Risk</div>
            <div class="var-metric-value ${riskClass}">NPR ${data.varValue.toFixed(2)}</div>
          </div>
          <div class="var-metric">
            <div class="var-metric-label">Risk Percentage</div>
            <div class="var-metric-value ${riskClass}">${varPercentage.toFixed(2)}%</div>
          </div>
          <div class="var-metric">
            <div class="var-metric-label">Confidence Level</div>
            <div class="var-metric-value">${data.confidenceLevel.toFixed(1)}%</div>
          </div>
          <div class="var-metric">
            <div class="var-metric-label">Investment Period</div>
            <div class="var-metric-value">${periodDisplay}</div>
          </div>
          <div class="var-metric">
            <div class="var-metric-label">Risk Category</div>
            <div class="var-metric-value">
              <span class="risk-badge ${riskClass}">${riskLevel}</span>
            </div>
          </div>
        </div>

        <!-- VaR Risk Gauge -->
        <div class="var-risk-gauge">
          <div class="var-gauge-container">
            <div class="var-gauge-needle" style="transform: translateX(-50%) rotate(${this.calculateGaugeAngle(varPercentage)}deg)"></div>
          </div>
          
          <div class="var-gauge-labels">
            <div class="var-gauge-label">Low<br>0-5%</div>
            <div class="var-gauge-label">Medium<br>5-15%</div>
            <div class="var-gauge-label">High<br>15%+</div>
          </div>
        </div>

        <!-- Investment Recommendation -->
        <div class="recommendation-section">
          <div class="section-title">
            Investment Recommendation
          </div>
          <div class="recommendation-card ${riskClass}">
            ${this.getVarRecommendation(data, varPercentage, periodDisplay)}
          </div>
        </div>
      </div>
    `;

    this.resultsDiv.classList.add('show');
    this.resultsDiv.scrollIntoView({ behavior: "smooth" });
  }

  formatPeriodDisplay(periodValue, periodUnit, daysOfInvestment) {
    // Show both the original input and days equivalent
    if (periodUnit === "days") {
      return `${periodValue} ${periodUnit}`;
    } else {
      const unitSingular = periodUnit.slice(0, -1); // Remove 's' from plural
      const displayUnit = periodValue === 1 ? unitSingular : periodUnit;
      return `${periodValue} ${displayUnit} (${daysOfInvestment} days)`;
    }
  }

  calculateGaugeAngle(varPercentage) {
    const maxRisk = 20;
    const normalizedRisk = Math.min(varPercentage, maxRisk) / maxRisk;
    const angle = (normalizedRisk * 180) - 90;
    return angle;
  }

  getRiskLevel(varPercentage) {
    if (varPercentage <= 5) return "Low Risk";
    if (varPercentage <= 15) return "Medium Risk";
    return "High Risk";
  }

  getRiskClass(varPercentage) {
    if (varPercentage <= 5) return "risk-low";
    if (varPercentage <= 15) return "risk-medium";
    return "risk-high";
  }

  getVarRecommendation(data, varPercentage, periodDisplay) {
    let recommendation = "";

    if (varPercentage <= 5) {
      recommendation = "Conservative Investment - Low risk with stable returns expected";
    } else if (varPercentage <= 15) {
      recommendation = "Moderate Investment - Balanced risk-return profile";
    } else {
      recommendation = "High Risk Investment - Careful consideration required";
    }

    return `
      <div class="recommendation-content">
        <span>${recommendation}</span>
      </div>
      <div class="recommendation-details">
        <small>Based on ${data.confidenceLevel.toFixed(1)}% confidence level over ${periodDisplay} using Monte Carlo simulation</small>
      </div>
    `;
  }

  showError(message) {
    this.resultsDiv.innerHTML = `
      <div class="var-error">
        <span>${message}</span>
      </div>
    `;
    this.resultsDiv.classList.add('show');
  }
}

// Initialize classes
let varStockDropdown;
let varFormHandler;
document.addEventListener("DOMContentLoaded", function () {
  varStockDropdown = new VarStockDropdown();
  varFormHandler = new VarCalculationForm();
});