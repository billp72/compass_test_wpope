import { useState, useEffect } from 'react'

import './App.css'

function FilterControls({
  operatorName,
  valueName,
  operator,
  value,
  onChange,
}) {
  return (
    <div>
      <select name={operatorName} value={operator} onChange={onChange}>
        <option value="equals">Equals</option>
        <option value="greater">Greater than</option>
        <option value="less">Less than</option>
      </select>

      <input
        type="number"
        name={valueName}
        value={value}
        onChange={onChange}
        placeholder="Value"
      />
    </div>
  );
}

const initialFilters = {
  keyword: "",
  address: "",
  city: "",
  state: "",
  priceOperator: "equals",
  price: "",
  bedroomsOperator: "equals",
  bedrooms: "",
  bathroomsOperator: "equals",
  bathrooms: "",
  status: "",
};

function App() {
  const [filters, setFilters] = useState(initialFilters);
  const [pagination, setPagination] = useState({
    page: 1,
    pageSize: 3,
    totalPages: 1,
  });
  const [filteredListings, setFilteredListings] = useState([]);

  function updateFilter(event) {
    const { name, value } = event.target;

    setFilters((currentFilters) => ({
      ...currentFilters,
      [name]: value,
    }));
  }

  useEffect(() => {
    const params = new URLSearchParams();

    // Add keyword search when present.
    if (filters.keyword?.trim() && filters.keyword?.trim().length > 2) {
      params.set("keyword", filters.keyword.trim());
    }

    // Add text filters.
    if (filters.city?.trim()) {
      params.append(
        "filter",
        `city:EQUALS:${filters.city.trim()}`
      );
    }

    if (filters.address?.trim()) {
      params.append(
        "filter",
        `address:EQUALS:${filters.address.trim()}`
      );
    }

    if (filters.state?.trim()) {
      params.append(
        "filter",
        `state:EQUALS:${filters.state.trim()}`
      );
    }

    if (filters.status?.trim()) {
      params.append(
        "filter",
        `status:EQUALS:${filters.status.trim()}`
      );
    }

    // Add numeric filters.
    if (filters.price !== "") {
      params.append(
        "filter",
        `price:${toBackendOperator(filters.priceOperator)}:${filters.price}`
      );
    }

    if (filters.bathrooms !== "") {
      params.append(
        "filter",
        `bathrooms:${toBackendOperator(filters.bathroomsOperator)}:${filters.bathrooms}`
      );
    }

    if (filters.bedrooms !== "") {
      params.append(
        "filter",
        `bedrooms:${toBackendOperator(filters.bedroomsOperator)}:${filters.bedrooms}`
      );
    }

    fetch(`/api/listings?${params.toString()}&page=${pagination.page}&pageSize=${pagination.pageSize}&totalPages=${pagination.totalPages}`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }

        return response.json();
      })
      .then((data) => {
        setFilteredListings(data.results);
        setPagination(data.pagination);
      })
      .catch((error) => {
        console.error("Error fetching listings:", error);
      });

  }, [filters, pagination])

  function toBackendOperator(operator) {
    switch (operator) {
      case "equals":
        return "EQUALS";
      case "greater":
        return "GREATER_THAN";
      case "less":
        return "LESS_THAN";
      default:
        throw new Error(`Unsupported filter operator: ${operator}`);
    }
  }

  return (
    <>
      <section id="center">

        <div>
          <h1>Search Listings</h1>
          <p>
            <input 
              type="text" 
              name="keyword"
              onChange={updateFilter} 
              value={filters.keyword} 
              placeholder="Enter text" 
            />
          </p>
        </div>

      </section>

      <div className="ticks"></div>

      <section id="next-steps">
        <table>
          <thead>
            <tr>
              <th>
                Address
                <input
                  name="address"
                  value={filters.address}
                  onChange={updateFilter}
                  placeholder="Search address"
                />
              </th>

              <th>
                City
                <input
                  name="city"
                  value={filters.city}
                  onChange={updateFilter}
                  placeholder="Search city"
                />
              </th>

              <th>
                State
                <input
                  name="state"
                  value={filters.state}
                  onChange={updateFilter}
                  placeholder="Search State"
                />
              </th>

              <th>
                Price
                <FilterControls
                  operatorName="priceOperator"
                  valueName="price"
                  operator={filters.priceOperator}
                  value={filters.price}
                  onChange={updateFilter}
                />
              </th>

              <th>
                Bedrooms
                <FilterControls
                  operatorName="bedroomsOperator"
                  valueName="bedrooms"
                  operator={filters.bedroomsOperator}
                  value={filters.bedrooms}
                  onChange={updateFilter}
                />
              </th>
              <th>
                Bathrooms
                <FilterControls
                  operatorName="bathroomsOperator"
                  valueName="bathrooms"
                  operator={filters.bathroomsOperator}
                  value={filters.bathrooms}
                  onChange={updateFilter}
                />
              </th>
              <th>
                Status
                <input
                  name="status"
                  value={filters.status}
                  onChange={updateFilter}
                  placeholder="Search status"
                />
              </th>
            </tr>
          </thead>

          <tbody>
            {filteredListings.map((listing) => (
              <tr key={listing.id}>
                <td>{listing.address}</td>
                <td>{listing.city}</td>
                <td>{listing.state}</td>
                <td>{listing.price.toLocaleString()}</td>
                <td>{listing.bedrooms}</td>
                <td>{listing.bathrooms}</td>
                <td>{listing.status}</td>
              </tr>
            ))}

            {filteredListings.length === 0 && (
              <tr>
                <td colSpan="5">No listings found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      <div className="ticks">
        {Array.from({ length: pagination.totalPages }, (_, index) => (
          pagination.totalPages == 3 ? (
            <button onClick={() => setPagination({ ...pagination, page: index + 1 })} key={index} className="tick">{index + 1}</button>
          ) : <div className="tick">...</div>
        ))}
      </div>
      <section id="spacer"></section>
    </>
  )
}

export default App
