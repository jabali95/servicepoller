import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
import { Link } from 'react-router-dom';

class ServiceList extends Component {

  constructor(props) {
    super(props);
    this.state = {groups: [], isLoading: true};
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    this.setState({isLoading: true});

    fetch('api/services')
      .then(response => response.json())
      .then(data => this.setState({groups: data, isLoading: false}));
  }

  async remove(group) {
    await fetch('/api/services/' + group, {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(group),
    }).then(() => {
      let updatedGroups = [...this.state.groups].filter(i => i.url !== group.url);
      this.setState({groups: updatedGroups});
    });
  }

  render() {
    const {groups, isLoading} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const groupList = groups.map(group => {
      return <tr key={group.id}>
          <td style={{whiteSpace: 'nowrap'}}>{group.name}</td>
        <td style={{whiteSpace: 'nowrap'}}>{group.url}</td>
        <td style={{whiteSpace: 'nowrap'}}>{group.created}</td>
        <td style={{whiteSpace: 'nowrap'}}>{group.modified}</td>
        <td style={{whiteSpace: 'nowrap'}}>{group.status}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/services/" + group.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove(group)}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    return (
      <div>
        <Container fluid>
          <div className="float-right">
            <Button color="success" tag={Link} to="/services/new">Add Service</Button>
          </div>
          <h3>Kry Service Poller
          </h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="20%">Name</th>
              <th width="10%">URL</th>
              <th width="10%">Created</th>
              <th width="10%">Modified </th>
              <th width="10%">Status</th>
              <th width="10%">Actions</th>
            </tr>
            </thead>
            <tbody>
            {groupList}
            </tbody>
          </Table>
        </Container>
      </div>
    );
  }
}

export default ServiceList;