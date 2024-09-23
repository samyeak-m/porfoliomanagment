document.addEventListener("DOMContentLoaded", async () => {
    const form = document.getElementById("stock-form");
    const stockDataSection = document.getElementById("stock-data");

    // Fetch today's data on page load
    try {
        const todayResponse = await fetch('/api/daily-data/today');
        if (todayResponse.ok) {
            const todayData = await todayResponse.json();
            if (Array.isArray(todayData) && todayData.length > 0) {
                renderTable(todayData);
            } else {
                stockDataSection.innerHTML = "<p>No data available for today.</p>";
            }
        } else {
            throw new Error('Failed to fetch today\'s data');
        }
    } catch (error) {
        console.error("Error fetching today's data:", error);
        stockDataSection.innerHTML = "<p>Error fetching today's data. Please try again later.</p>";
    }

    // Handle form submission for filtering data
    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const symbol = document.getElementById("symbol").value.trim();
        const startDate = document.getElementById("start-date").value;
        const endDate = document.getElementById("end-date").value;

        // Construct URL dynamically based on whether symbol, startDate, or endDate is provided
        let url = '/api/daily-data/data';
        const params = [];

        if (symbol) params.push(`symbol=${symbol}`);
        if (startDate) params.push(`startDate=${startDate}`);
        if (endDate) params.push(`endDate=${endDate}`);

        if (params.length > 0) {
            url += `?${params.join('&')}`;
        }

        try {
            const response = await fetch(url);
            if (response.ok) {
                const data = await response.json();
                if (Array.isArray(data) && data.length > 0) {
                    renderTable(data);
                } else {
                    stockDataSection.innerHTML = "<p>No data found for the given criteria.</p>";
                }
            } else {
                throw new Error('Network response was not ok.');
            }
        } catch (error) {
            console.error("Error fetching stock data:", error);
            stockDataSection.innerHTML = "<p>Error fetching data. Please try again later.</p>";
        }
    });

    // Function to render data into a table
    function renderTable(data) {
        let table = `<table>
    <thead>
        <tr>
            ${data.some(item => item.date) ? '<th>Date</th>' : ''}
            ${data.some(item => item.symbol) ? '<th>Symbol</th>' : ''}
            <th>Close</th>
            <th>High</th>
            <th>Low</th>
            <th>Open</th>
        </tr>
    </thead>
    <tbody>`;

        data.forEach(item => {
            const formattedDate = item.date ? new Date(item.date).toISOString().split('T')[0] : null;
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

            table += `<tr>`;

            if (formattedDate) {
                table += `<td>${formattedDate}</td>`;
            }

            if (item.symbol) {
                table += `<td>${item.symbol}</td>`;
            }

            table += `
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
