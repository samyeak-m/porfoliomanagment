document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("stock-form");
    const stockDataSection = document.getElementById("stock-data");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const symbol = document.getElementById("symbol").value;
        const startDate = document.getElementById("start-date").value;
        const endDate = document.getElementById("end-date").value;

        try {
            const response = await fetch(`api/daily-data/symbol/${symbol}/date-range?startDate=${startDate}&endDate=${endDate}`);
            const data = await response.json();

            if (Array.isArray(data) && data.length > 0) {
                renderTable(data);
            } else {
                stockDataSection.innerHTML = "<p>No data found for the given criteria.</p>";
            }
        } catch (error) {
            console.error("Error fetching stock data:", error);
            stockDataSection.innerHTML = "<p>Error fetching data. Please try again later.</p>";
        }
    });

    function renderTable(data) {
        let table = `<table>
        <thead>
            <tr>
                <th>Date</th>
                <th>Close</th>
                <th>High</th>
                <th>Low</th>
                <th>Open</th>
            </tr>
        </thead>
        <tbody>`;

        data.forEach(item => {
            const formattedDate = new Date(item.date).toISOString().split('T')[0];
            const red = '#900000';
            const blue = '#01538f';
            const green = '#015201';
            const white = '#f1f1f1';

            let closeColor = blue;
            if (item.close < item.open) {
                closeColor = green;
            } else if (item.close > item.open) {
                closeColor = red;
            }

            table += `<tr>
            <td>${formattedDate}</td>
            <td style="background-color: ${closeColor};color:${white};">${item.close}</td>
            <td>${item.high}</td>
            <td>${item.low}</td>
            <td>${item.open}</td>
            
        </tr>`;
        });

        table += `</tbody></table>`;
        stockDataSection.innerHTML = table;
    }
});
