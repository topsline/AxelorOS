import React, { Component } from "react";

import { convertNumberToTime } from "./container";

export function translate(str) {
  if (window._t && typeof str === "string") {
    return window._t(str);
  }
  return str;
}

class Cell extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: {
        tasks: {},
      },
      isField: true,
    };
  }

  getData(taskIndex, duration) {
    const task = this.props.data.tasks[taskIndex];
    return {
      date: task.date,
      projectId: task.projectId,
      taskId: task.taskId,
      task: task.task,
      duration: parseFloat(duration === "" ? 0 : duration),
      projectTask: task.projectTask,
    };
  }

  componentDidMount() {
    this.setState({ data: this.props.data, isField: this.props.isField });
  }

  componentWillReceiveProps(nextProps) {
    this.setState({ data: nextProps.data, isField: nextProps.isField });
  }

  render() {
    const { isToday, cellBackGroundColor, optionValue, optionLabel } =
      this.props;
    let backgroundColor = this.props.cellBackgroundColor || "#FFFFFF";
    backgroundColor = this.props.isToday ? "aliceblue" : backgroundColor;
    const styles = {
      backgroundColor:
        cellBackGroundColor !== undefined && `${cellBackGroundColor}`,
      color: cellBackGroundColor && "white",
      borderTop: cellBackGroundColor && "0px !important",
    };
    const style = this.props.style || {};

    return (
      <div
        style={{
          position: "relative",
          minWidth: this.state.isField ? 125 : "auto",
          height: "100%",
          borderTop: "1px solid #DDDDDD",
          ...style,
        }}
      >
        <div style={{ backgroundColor }}>
          <div
            className="cell-height cell-header"
            style={{
              ...styles,
              padding: 10,
              paddingBottom: 20,
              height: 50,
              fontWeight: "bold",
              textAlign: "center",
            }}
            onClick={() => {
              if (this.props.headerClick) {
                this.props.headerClick();
              }
            }}
          >
            <span>{translate(this.props.header)}</span>
            <span className="sorting-component">
              {this.props.sortingComponent}
            </span>
          </div>
          <div className={this.props.bodyStyleClass}>
            {Object.keys(this.props.data).map((task, index) => (
              <div
                className="cell-height"
                key={index}
                style={{
                  ...styles,
                  borderTop: !cellBackGroundColor && "1px solid #DDDDDD",
                  overflowY: "auto",
                  overflow:
                    this.props.bodyStyleClass === "footer-context"
                      ? "hidden"
                      : "auto",
                  backgroundColor: cellBackGroundColor
                    ? styles.backgroundColor
                    : this.state.data[task].selected
                    ? "#abb9d3"
                    : index % 2 !== 0
                    ? "#FFFFFF"
                    : "#F2F2F2",
                  textAlign: "center",
                }}
              >
                <div
                  style={{
                    padding: "2px 5px 2px 7px",
                  }}
                >
                  {this.props.data[task][optionValue] &&
                    this.props.data[task][optionValue][optionLabel]}
                </div>
              </div>
            ))}
          </div>
          <div
            className={`task-footer`}
            style={{
              color: "white",
              borderTop: !cellBackGroundColor && "1px solid #DDDDDD",
              backgroundColor: "#334250",
              textAlign: "center",
            }}
          >
            {isToday ? (
              <div
                style={{
                  height: "inherit",
                  backgroundColor: "rgba(2,117,216, 0.3)",
                }}
              >
                <span
                  style={{
                    display: "block",
                    fontWeight: "bold",
                    paddingTop: 5,
                  }}
                >
                  {translate(this.props.footer) ||
                    convertNumberToTime(this.props.data.total)}
                </span>
              </div>
            ) : (
              <span
                style={{ display: "block", fontWeight: "bold", paddingTop: 5 }}
              >
                {translate(this.props.footer) ||
                  convertNumberToTime(this.props.data.total)}
              </span>
            )}
          </div>
        </div>
      </div>
    );
  }
}

export default Cell;
