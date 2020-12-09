import React, { Component } from 'react';
import './App.css';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import ServiceList from './ServiceList';
import ServiceEdit from './ServiceEdit';

class App extends Component {
  render() {
    return (
      <Router>
        <Switch>
          <Route path='/' exact={true} component={ServiceList}/>
          <Route path='/services' exact={true} component={ServiceList}/>
          <Route path='/services/:id' component={ServiceEdit}/>
        </Switch>
      </Router>
    )
  }
}

export default App;